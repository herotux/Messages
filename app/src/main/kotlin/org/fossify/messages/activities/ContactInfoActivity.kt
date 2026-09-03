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

class ContactInfoActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backgroundColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        val primaryColor = getProperPrimaryColor()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        val toolbar = Toolbar(this).apply {
            title = "اطلاعات تماس"
            setTitleTextColor(textColor)
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            textDirection = android.view.View.TEXT_DIRECTION_RTL
        }

        val title = TextView(this).apply {
            text = "ارتباط با HEROTUX"
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER
        }
        content.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "برای پشتیبانی، پیشنهادها و گزارش مشکلات با ما در تماس باشید."
            textSize = 14f
            setTextColor(textColor)
            alpha = 0.75f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        contactCard(content, "ایمیل", "thefreetux@gmail.com", primaryColor, textColor) {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thefreetux@gmail.com")))
        }
        contactCard(content, "تلفن پشتیبانی", "09375647544", primaryColor, textColor) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09375647544")))
        }
        contactCard(content, "وب‌سایت", "herotux.github.io", primaryColor, textColor) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io")))
        }

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        updateTextColors(root)
    }

    private fun contactCard(
        root: LinearLayout,
        label: String,
        value: String,
        primaryColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ) {
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

        val text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }
        text.addView(TextView(this).apply {
            this.text = label
            textSize = 13f
            setTextColor(textColor)
            alpha = 0.7f
        })
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
