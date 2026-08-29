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

        val backgroundColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        val primaryColor = getProperPrimaryColor()
        val horizontalPadding = dp(24)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        val toolbar = Toolbar(this).apply {
            title = "About"
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

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val brand = TextView(this).apply {
            text = "HEROTUX"
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
        }
        content.addView(brand, LinearLayout.LayoutParams(-1, -2))

        val edition = TextView(this).apply {
            text = "Messages — HEROTUX Edition"
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        content.addView(edition, LinearLayout.LayoutParams(-1, -2))

        val developer = TextView(this).apply {
            text = "Developed by Hamid Saydy"
            textSize = 15f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            alpha = 0.82f
            setPadding(0, dp(4), 0, 0)
        }
        content.addView(developer, LinearLayout.LayoutParams(-1, -2))

        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setContentPadding(dp(20), dp(18), dp(20), dp(18))
            strokeWidth = dp(1)
            strokeColor = primaryColor
            setCardBackgroundColor(backgroundColor)
        }

        val cardText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        cardText.addView(infoText("About this edition", 17f, true, textColor))
        cardText.addView(infoText("A lightweight and privacy-focused SMS and MMS messaging application developed and maintained by HEROTUX.", 15f, false, textColor))
        cardText.addView(infoText("Version ${BuildConfig.VERSION_NAME}", 14f, false, textColor))

        card.addView(cardText)
        val cardParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(28)
        }
        content.addView(card, cardParams)

        val source = TextView(this).apply {
            text = "View project on GitHub"
            textSize = 15f
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(22), dp(12), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/herotux/Messages")))
            }
        }
        content.addView(source, LinearLayout.LayoutParams(-1, -2))

        val footer = TextView(this).apply {
            text = "HEROTUX · Software Development"
            textSize = 13f
            setTextColor(textColor)
            alpha = 0.62f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        content.addView(footer, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        updateTextColors(root)
    }

    private fun infoText(text: String, size: Float, bold: Boolean, color: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
