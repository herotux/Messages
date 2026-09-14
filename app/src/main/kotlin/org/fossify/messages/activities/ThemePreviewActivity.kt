package org.fossify.messages.activities

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
import org.fossify.messages.helpers.ThemeResolver

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
        val tokens = ThemeResolver.resolve(theme)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        }
        val toolbar = Toolbar(this).apply {
            title = if (english) theme.nameEn else theme.nameFa
            setTitleTextColor(tokens.onPrimary)
            setBackgroundColor(tokens.toolbar)
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationIcon?.setTint(tokens.onPrimary)
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
            tokens.background,
            theme.gradientColors,
            theme.gradientAngle,
            theme.wallpaperUri
        )
        root.addView(conversation, LinearLayout.LayoutParams(-1, 0, 1f))

        conversation.addView(messageBubble(
            text = if (english) "Hi 👋 This is an incoming message." else "سلام 👋 این یک پیام دریافتی است",
            background = tokens.incomingMessage,
            textColor = tokens.messageText,
            alignEnd = false
        ), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, dp(56), dp(8)) })
        conversation.addView(TextView(this).apply {
            text = if (english) "Today · 10:24" else "امروز · ۱۰:۲۴"
            textSize = 11f
            setTextColor(tokens.messageSecondaryText)
            gravity = if (english) Gravity.START else Gravity.END
        }, LinearLayout.LayoutParams(-1, dp(24)))
        conversation.addView(messageBubble(
            text = if (english) "Hello! The new theme is ready 😊" else "سلام! تم جدید آماده است 😊",
            background = tokens.outgoingMessage,
            textColor = ThemeResolver.contrastColor(tokens.outgoingMessage),
            alignEnd = true
        ), LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(56), 0, 0, dp(6)) })
        conversation.addView(TextView(this).apply {
            text = if (english) "10:25  ✓✓" else "۱۰:۲۵  ✓✓"
            textSize = 11f
            setTextColor(tokens.messageSecondaryText)
            gravity = if (english) Gravity.END else Gravity.START
        }, LinearLayout.LayoutParams(-1, dp(24)))

        val composer = MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(tokens.surface)
        }
        val composerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        composerRow.addView(EditText(this).apply {
            hint = if (english) "Type a message…" else "نوشتن پیام…"
            setTextColor(tokens.onSurface)
            setHintTextColor(tokens.onSurfaceVariant)
            background = null
            setSingleLine(true)
            setPadding(dp(8), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        composerRow.addView(TextView(this).apply {
            text = "➤"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(ThemeResolver.contrastColor(tokens.fab))
            setBackgroundColor(tokens.fab)
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_THEME_ID = "theme_id"
    }
}
