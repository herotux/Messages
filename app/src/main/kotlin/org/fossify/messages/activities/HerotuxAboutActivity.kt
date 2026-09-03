package org.fossify.messages.activities

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_CONTACT_PAGE, false)) renderContactPage() else renderAboutPage()
    }

    private fun renderAboutPage() {
        val backgroundColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        val primaryColor = getProperPrimaryColor()
        val horizontalPadding = dp(24)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor) }
        val toolbar = Toolbar(this).apply {
            title = "درباره هما"
            setTitleTextColor(textColor)
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            elevation = dp(2).toFloat()
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(horizontalPadding, dp(24), horizontalPadding, dp(32))
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(TextView(this).apply {
            text = "هما"
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "پیام‌رسانک SMS و MMS"
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "توسعه‌یافته توسط HEROTUX"
            textSize = 15f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            alpha = 0.82f
            setPadding(0, dp(4), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setContentPadding(dp(20), dp(18), dp(20), dp(18))
            strokeWidth = dp(1)
            strokeColor = primaryColor
            setCardBackgroundColor(backgroundColor)
        }
        val cardText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardText.addView(infoText("درباره هما", 17f, true, textColor))
        cardText.addView(infoText("هما یک برنامه سبک و حریم‌خصوصی‌محور برای مدیریت پیامک و MMS است.", 15f, false, textColor))
        cardText.addView(infoText("نسخه ${BuildConfig.VERSION_NAME}", 14f, false, textColor))
        card.addView(cardText)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
        content.addView(TextView(this).apply {
            text = "اطلاعات تماس\nContact information"
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(22), dp(12), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { startActivity(Intent(this@HerotuxAboutActivity, HerotuxAboutActivity::class.java).putExtra(EXTRA_CONTACT_PAGE, true)) }
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "herotux.github.io"
            textSize = 14f
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) }
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "HEROTUX · Software Development"
            textSize = 13f
            setTextColor(textColor)
            alpha = 0.62f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        updateTextColors(root)
    }

    private fun renderContactPage() {
        val backgroundColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        val primaryColor = getProperPrimaryColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor) }
        val toolbar = Toolbar(this).apply {
            title = "اطلاعات تماس"
            setTitleTextColor(textColor)
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { setPadding(dp(20), dp(20), dp(20), dp(28)) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            textDirection = android.view.View.TEXT_DIRECTION_RTL
        }
        content.addView(TextView(this).apply {
            text = "ارتباط با هما"
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            text = "برای پشتیبانی، پیشنهادها و گزارش مشکلات با ما در تماس باشید."
            textSize = 14f
            setTextColor(textColor)
            alpha = 0.75f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }, LinearLayout.LayoutParams(-1, -2))
        contactCard(content, "ایمیل", "thefreetux@gmail.com", primaryColor, textColor) { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thefreetux@gmail.com"))) }
        contactCard(content, "تلفن پشتیبانی", "09375647544", primaryColor, textColor) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09375647544"))) }
        contactCard(content, "وب‌سایت", "herotux.github.io", primaryColor, textColor) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        updateTextColors(root)
    }

    private fun contactCard(root: LinearLayout, label: String, value: String, primaryColor: Int, textColor: Int, onClick: () -> Unit) {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = primaryColor
            setCardBackgroundColor(getProperBackgroundColor())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
        text.addView(TextView(this).apply { this.text = label; textSize = 13f; setTextColor(textColor); alpha = 0.7f })
        text.addView(TextView(this).apply {
            this.text = value
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(text)
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun infoText(text: String, size: Float, bold: Boolean, color: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object { private const val EXTRA_CONTACT_PAGE = "contact_page" }
}
