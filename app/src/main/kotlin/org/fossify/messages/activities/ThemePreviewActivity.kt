package org.fossify.messages.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.ThemeBackground
import org.fossify.messages.helpers.ThemeManager

/** Full-screen conversation preview for a theme. */
class ThemePreviewActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_THEME_ID)
        val theme = id?.let { ThemeManager.find(this, it) } ?: ThemeManager.current(this)
        render(theme)
    }

    private fun render(theme: ThemeManager.ThemeDefinition) {
        val english = config.useEnglish
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        }
        val toolbar = Toolbar(this).apply {
            title = if (english) theme.nameEn else theme.nameFa
            setTitleTextColor(theme.colors.textPrimary)
            setBackgroundColor(theme.colors.toolbar)
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationIcon?.setTint(theme.colors.textPrimary)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))

        val conversation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(12))
        }
        ThemeBackground.apply(
            conversation,
            theme.backgroundType,
            theme.colors.background,
            theme.gradientColors,
            theme.gradientAngle,
            theme.wallpaperUri
        )
        root.addView(conversation, LinearLayout.LayoutParams(-1, 0, 1f))

        conversation.addView(messageBubble(
            text = if (english) "Hi 👋 This is an incoming message." else "سلام 👋 این یک پیام دریافتی است",
            background = theme.colors.incomingBubble,
            textColor = theme.colors.textPrimary,
            alignEnd = false
        ), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, dp(56), dp(8)) })
        conversation.addView(TextView(this).apply {
            text = if (english) "Today · 10:24" else "امروز · ۱۰:۲۴"
            textSize = 11f
            setTextColor(theme.colors.textSecondary)
            gravity = if (english) Gravity.START else Gravity.END
        }, LinearLayout.LayoutParams(-1, dp(24)))
        conversation.addView(messageBubble(
            text = if (english) "Hello! The new theme is ready 😊" else "سلام! تم جدید آماده است 😊",
            background = theme.colors.outgoingBubble,
            textColor = readableText(theme.colors.outgoingBubble),
            alignEnd = true
        ), LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(56), 0, 0, dp(6)) })
        conversation.addView(TextView(this).apply {
            text = if (english) "10:25  ✓✓" else "۱۰:۲۵  ✓✓"
            textSize = 11f
            setTextColor(theme.colors.textSecondary)
            gravity = if (english) Gravity.END else Gravity.START
        }, LinearLayout.LayoutParams(-1, dp(24)))

        val composer = MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(theme.colors.surface)
        }
        val composerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        composerRow.addView(EditText(this).apply {
            hint = if (english) "Type a message…" else "نوشتن پیام…"
            setTextColor(theme.colors.textPrimary)
            setHintTextColor(theme.colors.textSecondary)
            background = null
            setSingleLine(true)
            setPadding(dp(8), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        composerRow.addView(TextView(this).apply {
            text = "➤"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(readableText(theme.colors.fab))
            setBackgroundColor(theme.colors.fab)
            setPadding(dp(10), 0, dp(10), 0)
        }, LinearLayout.LayoutParams(dp(48), dp(42)))
        composer.addView(composerRow)
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(8), dp(12), dp(12)) })
        setContentView(root)
    }

    private fun messageBubble(text: String, background: Int, textColor: Int, alignEnd: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        this.setTextColor(textColor)
        setPadding(dp(14), dp(11), dp(14), dp(11))
        gravity = if (alignEnd) Gravity.END else Gravity.START
        this.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(background)
            cornerRadius = dp(18).toFloat()
        }
    }

    private fun readableText(background: Int): Int {
        val luminance = (0.299 * Color.red(background) + 0.587 * Color.green(background) + 0.114 * Color.blue(background)) / 255.0
        return if (luminance > 0.55) Color.BLACK else Color.WHITE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_THEME_ID = "theme_id"
    }
}
